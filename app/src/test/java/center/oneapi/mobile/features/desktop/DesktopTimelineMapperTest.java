package center.oneapi.mobile.features.desktop;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import center.oneapi.mobile.ChatMessage;
import center.oneapi.mobile.navigation.AppSection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DesktopTimelineMapperTest {
    @Test
    public void fromSession_ordersUserLogAssistantTimeline() throws Exception {
        JSONObject session = new JSONObject()
                .put("messages", new JSONArray()
                        .put(new JSONObject().put("role", "assistant").put("text", "完成").put("createdAt", 3000))
                        .put(new JSONObject().put("role", "user").put("text", "开始").put("createdAt", 1000)))
                .put("logs", new JSONArray()
                        .put(new JSONObject().put("type", "shell_command").put("text", "ls").put("createdAt", 2000)));

        List<ChatMessage> messages = DesktopTimelineMapper.fromSession(AppSection.CODEX, session);

        assertEquals("user", messages.get(0).role);
        assertEquals("开始", messages.get(0).text);
        assertTrue(messages.get(1).log);
        assertTrue(messages.get(1).text.startsWith("正在执行 shell_command"));
        assertEquals("assistant", messages.get(2).role);
        assertEquals("完成", messages.get(2).text);
    }

    @Test
    public void fromSession_coalescesClaudeProcessFragments() throws Exception {
        JSONObject session = new JSONObject()
                .put("messages", new JSONArray()
                        .put(new JSONObject().put("role", "assistant").put("text", "正在").put("jobId", "j1").put("createdAt", 1000))
                        .put(new JSONObject().put("role", "assistant").put("text", "分析").put("jobId", "j1").put("createdAt", 1001)));

        List<ChatMessage> messages = DesktopTimelineMapper.fromSession(AppSection.CLAUDE, session);

        assertEquals(1, messages.size());
        assertEquals("正在分析", messages.get(0).text);
    }

    @Test
    public void fromJobEvents_filtersLowValueDesktopLifecycleLogs() throws Exception {
        JSONArray events = new JSONArray()
                .put(new JSONObject().put("type", "project").put("text", "D:\\WorkSpace\\codex").put("createdAt", 1000))
                .put(new JSONObject().put("type", "running").put("text", "Codex 已开始处理当前任务。").put("createdAt", 1001))
                .put(new JSONObject().put("type", "prepare").put("text", "thread.started").put("createdAt", 1002))
                .put(new JSONObject().put("type", "prepare").put("text", "turn.started").put("createdAt", 1003))
                .put(new JSONObject().put("type", "shell_command").put("text", "ls").put("createdAt", 1004));

        List<ChatMessage> messages = DesktopTimelineMapper.fromJobEvents(AppSection.CODEX, "job-1", events);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0).log);
        assertTrue(messages.get(0).text.startsWith("正在执行 shell_command"));
    }

    @Test
    public void fromJobEvents_keepsExecutionLogsAndAssistantDeltas() throws Exception {
        JSONArray events = new JSONArray()
                .put(new JSONObject().put("type", "message").put("role", "user").put("text", "修复同步").put("createdAt", 1000))
                .put(new JSONObject().put("type", "log").put("title", "读取文件").put("body", "rg mobile").put("createdAt", 1001))
                .put(new JSONObject().put("type", "message_delta").put("role", "assistant").put("text", "已完成").put("createdAt", 1002));

        List<ChatMessage> messages = DesktopTimelineMapper.fromJobEvents(AppSection.CODEX, "job-2", events);

        assertEquals(3, messages.size());
        assertEquals("user", messages.get(0).role);
        assertEquals("修复同步", messages.get(0).text);
        assertTrue(messages.get(1).log);
        assertTrue(messages.get(1).text.contains("读取文件"));
        assertEquals("assistant", messages.get(2).role);
        assertEquals("已完成", messages.get(2).text);
    }
}
