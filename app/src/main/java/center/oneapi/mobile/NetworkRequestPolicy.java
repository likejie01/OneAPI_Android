package center.oneapi.mobile;

final class NetworkRequestPolicy {
    enum Priority {
        CRITICAL,
        USER_ACTION,
        DESKTOP_SYNC,
        BACKGROUND
    }

    static final long WALLET_TTL_MS = 15_000L;
    static final long SUBSCRIPTIONS_TTL_MS = 5 * 60_000L;
    static final long SERVICE_STATUS_TTL_MS = 30_000L;
    static final long ANNOUNCEMENTS_TTL_MS = 5 * 60_000L;
    static final long DEFAULT_PANEL_TTL_MS = 60_000L;

    private NetworkRequestPolicy() {
    }

    static long panelTtlMs(String title) {
        String clean = title == null ? "" : title.trim();
        if ("钱包与消耗".equals(clean)) return WALLET_TTL_MS;
        if ("套餐订阅".equals(clean)) return SUBSCRIPTIONS_TTL_MS;
        if ("服务状态".equals(clean)) return SERVICE_STATUS_TTL_MS;
        if ("系统公告".equals(clean)) return ANNOUNCEMENTS_TTL_MS;
        return DEFAULT_PANEL_TTL_MS;
    }
}
