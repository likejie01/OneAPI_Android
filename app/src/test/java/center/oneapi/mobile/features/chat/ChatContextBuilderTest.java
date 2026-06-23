package center.oneapi.mobile.features.chat;

import org.json.JSONArray;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import center.oneapi.mobile.ChatMessage;

import static org.junit.Assert.assertEquals;

public class ChatContextBuilderTest {
    @Test
    public void build_countsOnlyValidMessagesWhenApplyingWindowLimit() throws Exception {
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            messages.add(new ChatMessage("user", "问题" + i, i));
            messages.add(ChatMessage.log("执行日志" + i, i));
            messages.add(new ChatMessage("assistant", "正在思考...", i));
        }

        JSONArray context = ChatContextBuilder.build(messages, "短上下文");

        assertEquals(8, context.length());
        assertEquals("问题4", context.getJSONObject(0).getString("content"));
        assertEquals("问题11", context.getJSONObject(7).getString("content"));
    }

    @Test
    public void build_removesThinkingBlocksAndImageUris() throws Exception {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("assistant", "<thinking>hidden</thinking>\n\nvisible", 1));
        messages.add(new ChatMessage("user", "content://image/1\n请分析", 2));

        JSONArray context = ChatContextBuilder.build(messages, "中上下文");

        assertEquals("visible", context.getJSONObject(0).getString("content"));
        assertEquals("请分析", context.getJSONObject(1).getString("content"));
    }
}
