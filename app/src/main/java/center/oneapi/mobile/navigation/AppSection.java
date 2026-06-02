package center.oneapi.mobile.navigation;

public enum AppSection {
    CHAT("chat", "Chat"),
    IMAGE("image", "Image"),
    CODEX("codex", "Codex"),
    CLAUDE("claude", "Claude"),
    SETTINGS("settings", "系统设置"),
    WALLET("wallet", "我的钱包"),
    SERVICE("service", "服务状态"),
    SUBSCRIPTIONS("subscriptions", "套餐订阅");

    public final String id;
    public final String label;

    AppSection(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public boolean isDesktop() {
        return this == CODEX || this == CLAUDE;
    }

    public boolean isConversation() {
        return this == CHAT || this == IMAGE || this == CODEX || this == CLAUDE;
    }
}
