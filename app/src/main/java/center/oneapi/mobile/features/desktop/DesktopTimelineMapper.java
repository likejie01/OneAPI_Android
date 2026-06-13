package center.oneapi.mobile.features.desktop;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import center.oneapi.mobile.ChatMessage;
import center.oneapi.mobile.DesktopTimelineRow;
import center.oneapi.mobile.navigation.AppSection;

public final class DesktopTimelineMapper {
    private DesktopTimelineMapper() {
    }

    public static List<ChatMessage> fromSession(AppSection section, JSONObject item) {
        List<DesktopTimelineRow> rows = new ArrayList<>();
        collectRows(section, rows, item == null ? null : item.optJSONArray("messages"), false);
        collectRows(section, rows, item == null ? null : item.optJSONArray("logs"), true);
        rows.sort(DesktopTimelineMapper::compareRows);
        LinkedHashMap<String, DesktopTimelineRow> rowsByStableKey = new LinkedHashMap<>();
        List<DesktopTimelineRow> uniqueRows = new ArrayList<>();
        for (DesktopTimelineRow row : rows) {
            if (!row.stableKey.isEmpty() && rowsByStableKey.containsKey(row.stableKey)) {
                continue;
            }
            boolean duplicate = false;
            for (DesktopTimelineRow existing : uniqueRows) {
                if (row.duplicates(existing)) {
                    duplicate = true;
                    break;
                }
            }
            if (duplicate) continue;
            uniqueRows.add(row);
            if (!row.stableKey.isEmpty()) {
                rowsByStableKey.put(row.stableKey, row);
            }
        }
        if (section == AppSection.CLAUDE) {
            uniqueRows = coalesceClaudeAssistantRows(uniqueRows);
        }
        return messagesFromRows(uniqueRows);
    }

    public static List<ChatMessage> fromJobEvents(AppSection section, String jobId, JSONArray events) {
        JSONArray messages = new JSONArray();
        JSONArray logs = new JSONArray();
        if (events != null) {
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.optJSONObject(i);
                if (event == null) continue;
                JSONObject item = new JSONObject();
                copyField(event, item, "id");
                copyField(event, item, "event_id");
                copyField(event, item, "eventId");
                copyField(event, item, "sessionId");
                copyField(event, item, "session_id");
                copyField(event, item, "role");
                copyField(event, item, "text");
                copyField(event, item, "body");
                copyField(event, item, "title");
                copyField(event, item, "type");
                copyField(event, item, "phase");
                copyField(event, item, "command");
                copyField(event, item, "createdAt");
                copyField(event, item, "created_at");
                copyField(event, item, "timestamp");
                try {
                    item.put("jobId", jobId);
                } catch (Exception ignored) {
                }
                String type = first(event, "type", "phase").toLowerCase(Locale.ROOT);
                String role = first(event, "role").toLowerCase(Locale.ROOT);
                if ("message".equals(type) || "message_delta".equals(type) || "user".equals(role) || "assistant".equals(role)) {
                    messages.put(item);
                } else {
                    logs.put(item);
                }
            }
        }
        List<DesktopTimelineRow> rows = new ArrayList<>();
        collectRows(section, rows, messages, false);
        collectRows(section, rows, logs, true);
        rows.sort(DesktopTimelineMapper::compareRows);
        return messagesFromRows(rows);
    }

    public static long normalizeTimestamp(long value) {
        if (value <= 0) return System.currentTimeMillis();
        return value > 10000000000L ? value : value * 1000L;
    }

    private static void collectRows(AppSection section, List<DesktopTimelineRow> out, JSONArray rows, boolean log) {
        if (rows == null) return;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.optJSONObject(i);
            if (item == null) continue;
            String role = item.optString("role", log ? "assistant" : "assistant");
            if (!"user".equals(role)) role = "assistant";
            String text = first(item, "text", "content", "body", "message", "title");
            if (text.isEmpty()) continue;
            long timestamp = normalizeTimestamp(firstLong(item, "timestamp", "createdAt", "created_at"));
            if (timestamp <= 0L) timestamp = System.currentTimeMillis();
            String stableId = first(item, "event_id", "id", "messageId", "message_id", "eventId");
            String jobId = first(item, "jobId", "job_id");
            if (stableId.isEmpty() && !jobId.isEmpty()) {
                stableId = (log ? "log:" : "message:") + jobId + ":" + role + ":" + normalizeForKey(text);
            }
            if (log) {
                String title = first(item, "title", "type", "phase");
                String command = first(item, "command");
                String type = first(item, "type");
                if (isLowValueLog(section, title, type, text)) continue;
                String logTitle = logTitle(section, title, type, command, text);
                StringBuilder body = new StringBuilder();
                body.append(logTitle);
                if (!command.isEmpty()) body.append("\n\n```bash\n").append(command).append("\n```");
                String path = first(item, "path", "file", "filePath", "cwd", "workspace");
                if (!path.isEmpty()) body.append("\n\n文件：`").append(path).append("`");
                if (!command.isEmpty() && looksLikeCommandJson(text)) {
                    // Command is already rendered above; Codex can repeat it in JSON body.
                } else if (looksLikeCodeLog(type, text)) {
                    body.append("\n\n```").append(codeLanguageForLog(type, text)).append("\n").append(text).append("\n```");
                } else {
                    body.append("\n\n").append(text);
                }
                out.add(new DesktopTimelineRow(ChatMessage.log(body.toString(), timestamp), stableId, timestamp, 1, i, jobId));
            } else {
                ChatMessage message = new ChatMessage(role, text, timestamp);
                out.add(new DesktopTimelineRow(message, stableId, timestamp, "user".equals(role) ? 0 : 2, i, jobId));
            }
        }
    }

    private static List<ChatMessage> messagesFromRows(List<DesktopTimelineRow> rows) {
        List<ChatMessage> out = new ArrayList<>();
        for (DesktopTimelineRow row : rows) {
            if (row != null && row.message != null) out.add(row.message);
        }
        return out;
    }

    private static int compareRows(DesktopTimelineRow a, DesktopTimelineRow b) {
        int byTime = Long.compare(a.timestamp, b.timestamp);
        if (byTime != 0) return byTime;
        int byPriority = Integer.compare(a.priority, b.priority);
        if (byPriority != 0) return byPriority;
        return Integer.compare(a.index, b.index);
    }

    private static List<DesktopTimelineRow> coalesceClaudeAssistantRows(List<DesktopTimelineRow> rows) {
        List<DesktopTimelineRow> out = new ArrayList<>();
        DesktopTimelineRow pending = null;
        boolean pendingMerged = false;
        for (DesktopTimelineRow row : rows) {
            if (canCoalesceClaudeAssistant(pending, row, pendingMerged)) {
                pending = mergeClaudeAssistantRows(pending, row);
                pendingMerged = true;
                continue;
            }
            if (pending != null) out.add(pending);
            pending = row;
            pendingMerged = false;
        }
        if (pending != null) out.add(pending);
        return out;
    }

    private static boolean canCoalesceClaudeAssistant(DesktopTimelineRow left, DesktopTimelineRow right, boolean leftAlreadyMerged) {
        if (!isClaudeAssistantProcessRow(left) || !isClaudeAssistantProcessRow(right)) return false;
        if (!left.jobId.isEmpty() && !right.jobId.isEmpty() && !left.jobId.equals(right.jobId)) return false;
        long gap = Math.abs(right.timestamp - left.timestamp);
        if (gap > 30000L) return false;
        return leftAlreadyMerged || likelyClaudeProcessFragment(left.message.text) || likelyClaudeProcessFragment(right.message.text);
    }

    private static boolean isClaudeAssistantProcessRow(DesktopTimelineRow row) {
        return row != null && row.message != null && !row.message.log && row.priority == 2
                && "assistant".equals(row.message.role);
    }

    private static boolean likelyClaudeProcessFragment(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) return false;
        if (text.length() <= 32) return true;
        return text.length() <= 80 && !text.contains("\n") && !endsWithSentenceBoundary(text);
    }

    private static boolean endsWithSentenceBoundary(String text) {
        if (text == null || text.isEmpty()) return false;
        char last = text.charAt(text.length() - 1);
        return "。！？!?；;：:\n".indexOf(last) >= 0;
    }

    private static DesktopTimelineRow mergeClaudeAssistantRows(DesktopTimelineRow left, DesktopTimelineRow right) {
        ChatMessage message = new ChatMessage("assistant",
                joinClaudeAssistantFragment(left.message.text, right.message.text),
                left.message.timestamp);
        String key = left.stableKey.isEmpty() ? right.stableKey : left.stableKey + "+" + right.stableKey;
        return new DesktopTimelineRow(message, key, left.timestamp, left.priority, left.index, left.jobId);
    }

    private static String joinClaudeAssistantFragment(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        char last = a.charAt(a.length() - 1);
        char first = b.charAt(0);
        if (Character.isWhitespace(last) || Character.isWhitespace(first)) return a + b;
        if (isAsciiWord(last) && isAsciiWord(first)) return a + " " + b;
        return a + b;
    }

    private static boolean isAsciiWord(char value) {
        return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z') || (value >= '0' && value <= '9');
    }

    private static boolean isLowValueLog(AppSection section, String title, String type, String text) {
        if (section != AppSection.CODEX && section != AppSection.CLAUDE) return false;
        String value = ((title == null ? "" : title) + " " + (type == null ? "" : type) + " " + (text == null ? "" : text)).toLowerCase(Locale.ROOT);
        return value.contains("输出已结束")
                || value.contains("已完成本次回复")
                || value.contains("正在整理会话记录")
                || value.contains("整理会话记录")
                || value.contains("output has ended")
                || value.contains("finished this reply")
                || value.contains("completed response")
                || value.contains("output ended")
                || value.contains("finish_reason")
                || value.trim().equals("complete")
                || value.trim().equals("done");
    }

    private static String logTitle(AppSection section, String title, String type, String command, String text) {
        String raw = ((title == null ? "" : title) + " " + (type == null ? "" : type) + " " + (command == null ? "" : command) + " " + (text == null ? "" : text)).toLowerCase(Locale.ROOT);
        if (section == AppSection.CODEX) {
            if (raw.contains("shell_command") || raw.contains("shell") || raw.contains("bash") || raw.contains("powershell") || raw.contains("cmd.exe")) {
                return "正在执行 shell_command";
            }
            if (raw.contains("apply_patch") || raw.contains("patch")) return "正在修改文件";
            if (raw.contains("read") || raw.contains("open") || raw.contains("cat") || raw.contains("rg ") || raw.contains("grep")) return "正在读取项目文件";
            if (raw.contains("tool") || raw.contains("function")) return "正在执行工具";
            if (raw.contains("error") || raw.contains("失败")) return "执行失败";
            return title == null || title.trim().isEmpty() ? "执行日志" : title.trim();
        }
        if (title != null && !title.trim().isEmpty()) return title.trim();
        if (type != null && !type.trim().isEmpty()) return type.trim();
        return "执行日志";
    }

    private static boolean looksLikeCodeLog(String type, String text) {
        String cleanType = type == null ? "" : type.toLowerCase(Locale.ROOT);
        String clean = text == null ? "" : text;
        return cleanType.contains("file") || cleanType.contains("patch") || cleanType.contains("diff")
                || clean.contains("```") || clean.contains("class ") || clean.contains("function ")
                || clean.contains("import ") || clean.contains("package ") || clean.contains("@@");
    }

    private static String codeLanguageForLog(String type, String text) {
        String value = (type == null ? "" : type.toLowerCase(Locale.ROOT)) + " " + (text == null ? "" : text.toLowerCase(Locale.ROOT));
        if (value.contains("diff") || value.contains("@@")) return "diff";
        if (value.contains("json")) return "json";
        if (value.contains("java")) return "java";
        if (value.contains("kotlin") || value.contains(".kt")) return "kotlin";
        if (value.contains("typescript") || value.contains(".ts")) return "typescript";
        if (value.contains("javascript") || value.contains(".js")) return "javascript";
        return "";
    }

    private static boolean looksLikeCommandJson(String text) {
        String clean = text == null ? "" : text.trim();
        return clean.startsWith("{") && clean.endsWith("}") && clean.contains("\"command\"");
    }

    private static String normalizeForKey(String value) {
        return (value == null ? "" : value).trim().replaceAll("\\s+", " ");
    }

    private static void copyField(JSONObject from, JSONObject to, String key) {
        if (from == null || to == null || key == null || !from.has(key)) return;
        try {
            to.put(key, from.opt(key));
        } catch (Exception ignored) {
        }
    }

    private static String first(JSONObject item, String... keys) {
        if (item == null) return "";
        for (String key : keys) {
            String value = item.optString(key, "").trim();
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static long firstLong(JSONObject item, String... keys) {
        if (item == null) return 0L;
        for (String key : keys) {
            if (!item.has(key)) continue;
            long value = item.optLong(key, 0L);
            if (value > 0L) return value;
        }
        return 0L;
    }
}
