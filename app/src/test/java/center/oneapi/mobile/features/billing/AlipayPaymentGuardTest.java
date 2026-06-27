package center.oneapi.mobile.features.billing;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AlipayPaymentGuardTest {
    @Test
    public void onlyAllowsSharedClientRechargeAmounts() {
        assertTrue(AlipayPaymentGuard.isAllowedAmount(50));
        assertTrue(AlipayPaymentGuard.isAllowedAmount(100));
        assertTrue(AlipayPaymentGuard.isAllowedAmount(200));
        assertTrue(AlipayPaymentGuard.isAllowedAmount(500));

        assertFalse(AlipayPaymentGuard.isAllowedAmount(10));
        assertFalse(AlipayPaymentGuard.isAllowedAmount(51));
        assertFalse(AlipayPaymentGuard.isAllowedAmount(0));
    }

    @Test
    public void blocksSecondPaymentForSameAccountWithinOneMinuteUntilCleared() {
        AlipayPaymentGuard guard = new AlipayPaymentGuard();

        assertTrue(guard.tryStart("user-a", 50, 1_000L).allowed);
        AlipayPaymentGuard.Decision blocked = guard.tryStart("user-a", 100, 30_000L);

        assertFalse(blocked.allowed);
        assertEquals("同一账号已有未完成支付，请稍后再试。", blocked.message);

        guard.clear("user-a");
        assertTrue(guard.tryStart("user-a", 100, 31_000L).allowed);
    }

    @Test
    public void debounceIsAccountScopedAndExpiresAfterOneMinute() {
        AlipayPaymentGuard guard = new AlipayPaymentGuard();

        assertTrue(guard.tryStart("user-a", 50, 1_000L).allowed);
        assertTrue(guard.tryStart("user-b", 50, 2_000L).allowed);
        assertTrue(guard.tryStart("user-a", 50, 61_001L).allowed);
    }
}
