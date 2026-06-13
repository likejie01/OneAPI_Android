package center.oneapi.mobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class SectionState {
    final List<ChatSession> sessions;
    final List<String> availableSkills;
    final Map<String, ExtensionRef> availableExtensionRefs = new LinkedHashMap<>();
    final List<String> availableModels = new ArrayList<>();
    final List<String> availableAssistants = new ArrayList<>();
    final Map<String, Integer> assistantOrders = new HashMap<>();
    final Set<String> favoriteModels = new LinkedHashSet<>();
    final Set<String> favoriteAssistants = new LinkedHashSet<>();
    final Set<String> favoriteExtensions = new LinkedHashSet<>();
    final Set<String> selectedSkills = new LinkedHashSet<>();
    final Set<String> pinnedSessionIds = new LinkedHashSet<>();
    final Set<String> deletedSessionIds = new LinkedHashSet<>();
    final Map<String, String> renamedSessionTitles = new HashMap<>();
    int selectedIndex;
    boolean fullPermission;
    String selectedModel = "gpt-5.4";
    String selectedReasoning = "中";
    String contextWindow = "自动";
    String imageSize = "1024x1024";
    String imageQuality = "medium";
    String selectedSessionId = "";

    SectionState(List<ChatSession> sessions, List<String> availableSkills) {
        this.sessions = new ArrayList<>(sessions);
        this.availableSkills = new ArrayList<>(availableSkills);
        for (String skill : this.availableSkills) {
            availableExtensionRefs.put(skill, new ExtensionRef(skill, inferExtensionKind(skill), skill, "", "shared"));
        }
        for (ChatSession session : this.sessions) {
            if (!session.assistantLabel.isEmpty() && !availableAssistants.contains(session.assistantLabel)) {
                availableAssistants.add(session.assistantLabel);
                assistantOrders.put(session.assistantLabel, 100);
            }
        }
    }

    ChatSession current() {
        if (sessions.isEmpty()) {
            return new ChatSession("empty", "空会话", "", "", new ArrayList<>());
        }
        if (!selectedSessionId.isEmpty()) {
            for (int i = 0; i < sessions.size(); i++) {
                if (selectedSessionId.equals(sessions.get(i).id)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        selectedIndex = Math.max(0, Math.min(selectedIndex, sessions.size() - 1));
        ChatSession selected = sessions.get(selectedIndex);
        selectedSessionId = selected.id;
        return selected;
    }

    private static String inferExtensionKind(String value) {
        String clean = value == null ? "" : value.toLowerCase(Locale.ROOT);
        if (clean.startsWith("/") || clean.startsWith("command:") || clean.startsWith("cmd:")) return "command";
        if (clean.startsWith("plugin:") || clean.contains("plugin")) return "plugin";
        return "skill";
    }
}
