package center.oneapi.mobile.features.billing;

import java.util.HashMap;
import java.util.Map;

public final class AlipayPaymentGuard {
    public static final int[] ALLOWED_AMOUNTS = {50, 100, 200, 500};
    private static final long WINDOW_MS = 60_000L;

    private final Map<String, Long> startedAtByAccount = new HashMap<>();

    public static boolean isAllowedAmount(int amount) {
        for (int allowed : ALLOWED_AMOUNTS) {
            if (allowed == amount) return true;
        }
        return false;
    }

    public synchronized Decision tryStart(String accountKey, int amount, long nowMs) {
        if (!isAllowedAmount(amount)) {
            return Decision.blocked("请选择 50、100、200 或 500 元充值。");
        }
        String key = cleanAccount(accountKey);
        Long startedAt = startedAtByAccount.get(key);
        if (startedAt != null && nowMs - startedAt < WINDOW_MS) {
            return Decision.blocked("同一账号已有未完成支付，请稍后再试。");
        }
        startedAtByAccount.put(key, nowMs);
        return Decision.allowed();
    }

    public synchronized void clear(String accountKey) {
        startedAtByAccount.remove(cleanAccount(accountKey));
    }

    private static String cleanAccount(String accountKey) {
        String clean = accountKey == null ? "" : accountKey.trim();
        return clean.isEmpty() ? "anonymous" : clean;
    }

    public static final class Decision {
        public final boolean allowed;
        public final String message;

        private Decision(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }

        static Decision allowed() {
            return new Decision(true, "");
        }

        static Decision blocked(String message) {
            return new Decision(false, message);
        }
    }
}
