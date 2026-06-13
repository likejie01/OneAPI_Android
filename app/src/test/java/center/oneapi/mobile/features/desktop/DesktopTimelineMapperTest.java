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
}
