package center.oneapi.mobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class DesktopSessionSync {
    static final int RECENT_PROJECT_LIMIT = 5;
    static final int RECENT_SESSION_LIMIT = 5;
    private static final long PENDING_LOCAL_SESSION_TTL_MS = 120_000L;

    private DesktopSessionSync() {
    }

    static List<ChatSession> mergePendingDesktopMessages(List<ChatSession> localSessions, List<ChatSession> serverSessions, long now) {
        List<ChatSession> merged = serverSessions == null ? new ArrayList<>() : new ArrayList<>(serverSessions);
        Map<String, ChatSession> localById = new HashMap<>();
        if (localSessions != null) {
            for (ChatSession local : localSessions) {
                if (local != null && local.id != null) localById.put(local.id, local);
            }
        }
        for (ChatSession server : merged) {
            if (server == null || server.id == null) continue;
            ChatSession local = localById.remove(server.id);
            if (local == null || local.messages == null || local.messages.isEmpty()) continue;
            for (ChatMessage message : local.messages) {
                if (message == null) continue;
                if (containsSimilarMessage(server.messages, message)) continue;
                if (now - message.timestamp <= PENDING_LOCAL_SESSION_TTL_MS || "user".equals(message.role)) {
                    server.messages.add(message);
                }
            }
            server.messages.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
        }
        for (ChatSession local : localById.values()) {
            if (shouldKeepLocalPendingSession(local, now)) {
                merged.add(local);
            }
        }
        return merged;
    }

    static LinkedHashMap<String, List<ChatSession>> recentProjectGroups(List<ChatSession> sessions, int projectLimit, int sessionLimit) {
        Map<String, List<ChatSession>> grouped = new HashMap<>();
        if (sessions != null) {
            for (ChatSession session : sessions) {
                if (session == null) continue;
                String key = session.projectLabel == null || session.projectLabel.trim().isEmpty()
                        ? "本机项目"
                        : session.projectLabel.trim();
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(session);
            }
        }
        List<Map.Entry<String, List<ChatSession>>> entries = new ArrayList<>(grouped.entrySet());
        for (Map.Entry<String, List<ChatSession>> entry : entries) {
            entry.getValue().sort((a, b) -> Long.compare(newestMessageTimestamp(b), newestMessageTimestamp(a)));
        }
        entries.sort((a, b) -> Long.compare(newestTimestamp(b.getValue()), newestTimestamp(a.getValue())));
        LinkedHashMap<String, List<ChatSession>> out = new LinkedHashMap<>();
        int maxProjects = projectLimit <= 0 ? entries.size() : Math.min(projectLimit, entries.size());
        for (int i = 0; i < maxProjects; i++) {
            Map.Entry<String, List<ChatSession>> entry = entries.get(i);
            List<ChatSession> rows = entry.getValue();
            int maxSessions = sessionLimit <= 0 ? rows.size() : Math.min(sessionLimit, rows.size());
            out.put(entry.getKey(), new ArrayList<>(rows.subList(0, maxSessions)));
        }
        return out;
    }

    static boolean containsSimilarMessage(List<ChatMessage> messages, ChatMessage target) {
        if (messages == null || target == null) return false;
        for (ChatMessage item : messages) {
            if (item == null) continue;
            if (!String.valueOf(item.role).equals(String.valueOf(target.role))) continue;
            if (Math.abs(item.timestamp - target.timestamp) > 10_000L) continue;
            String left = item.text == null ? "" : item.text.trim();
            String right = target.text == null ? "" : target.text.trim();
            if (left.equals(right)) return true;
            if (!left.isEmpty() && !right.isEmpty() && (left.contains(right) || right.contains(left))) return true;
        }
        return false;
    }

    static long newestMessageTimestamp(ChatSession session) {
        long newest = 0L;
        if (session == null || session.messages == null) return newest;
        for (ChatMessage message : session.messages) {
            if (message != null && message.timestamp > newest) newest = message.timestamp;
        }
        return newest;
    }

    private static boolean shouldKeepLocalPendingSession(ChatSession session, long now) {
        if (session == null || session.messages == null || session.messages.isEmpty()) return false;
        long newest = newestMessageTimestamp(session);
        if (newest <= 0L || now - newest > PENDING_LOCAL_SESSION_TTL_MS) return false;
        String status = session.status == null ? "" : session.status.trim().toLowerCase(Locale.ROOT);
        return "queued".equals(status)
                || "claimed".equals(status)
                || "running".equals(status)
                || "waiting_interaction".equals(status)
                || "pending".equals(status);
    }

    private static long newestTimestamp(List<ChatSession> sessions) {
        long newest = 0L;
        if (sessions == null) return newest;
        for (ChatSession session : sessions) {
            newest = Math.max(newest, newestMessageTimestamp(session));
        }
        return newest;
    }
}
